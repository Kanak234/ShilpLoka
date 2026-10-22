#!/usr/bin/env python3
# ShilpLoka desktop launcher (used by the Snap package).
#
# WHY: ShilpLoka is a Three.js game that Vite builds into ONE offline HTML file
#      (dist/index.html). On the desktop we want a real app window, not a browser
#      tab, and saves that survive restarts.
# WHAT: a small GTK 3 window with a WebKitGTK 4.1 web view that loads the game file.
#      WebGL and pointer lock (mouse look) are enabled; localStorage (the game's save
#      slot "shilploka.save.v1") is kept in the snap's per-user data folder.
# WHERE: snapcraft.yaml -> apps.shilploka.command runs this file; it loads
#      $SNAP/game/index.html, which is the unchanged output of `npm run build`.

import os
import sys

import gi

gi.require_version("Gtk", "3.0")
gi.require_version("Gdk", "3.0")
gi.require_version("WebKit2", "4.1")
from gi.repository import Gdk, Gtk, WebKit2  # noqa: E402

APP_TITLE = "ShilpLoka: Ancient Indian Voxel Sandbox | शिल्पलोक"

# WHERE the game file lives: inside the snap, or next to this script when run from the repo.
SNAP = os.environ.get("SNAP", "")
GAME_HTML = (
    os.path.join(SNAP, "game", "index.html")
    if SNAP
    else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "dist", "index.html")
)

# WHERE saves go: $SNAP_USER_DATA is ~/snap/shilploka/<revision> and is copied forward
# on every update, so a player's world is kept across new releases.
DATA_ROOT = os.environ.get("SNAP_USER_DATA") or os.path.join(
    os.path.expanduser("~"), ".local", "share", "shilploka"
)
CACHE_ROOT = os.environ.get("SNAP_USER_COMMON") or os.path.join(
    os.path.expanduser("~"), ".cache", "shilploka"
)


def build_webview():
    """WebView with persistent storage and the settings a WebGL game needs."""
    manager = WebKit2.WebsiteDataManager(
        base_data_directory=os.path.join(DATA_ROOT, "webkit-data"),
        base_cache_directory=os.path.join(CACHE_ROOT, "webkit-cache"),
    )
    context = WebKit2.WebContext.new_with_website_data_manager(manager)
    # WHY: the game is a local file; disk cache only slows the first start.
    context.set_cache_model(WebKit2.CacheModel.DOCUMENT_VIEWER)

    view = WebKit2.WebView.new_with_context(context)
    s = view.get_settings()
    s.set_enable_webgl(True)                       # Three.js renders with WebGL
    s.set_hardware_acceleration_policy(WebKit2.HardwareAccelerationPolicy.ALWAYS)
    s.set_enable_javascript(True)
    s.set_allow_file_access_from_file_urls(True)   # single-file build, loaded via file://
    s.set_enable_developer_extras(False)
    s.set_enable_write_console_messages_to_stdout(True)
    s.set_media_playback_requires_user_gesture(False)

    # WHY: no browser chrome in a game - block the right-click "Back/Reload" menu,
    #      because the game uses right click to place blocks.
    view.connect("context-menu", lambda *_: True)
    return view


def main():
    if not os.path.exists(GAME_HTML):
        sys.stderr.write(f"ShilpLoka: game file not found: {GAME_HTML}\n")
        return 1

    win = Gtk.Window(title=APP_TITLE)
    win.set_default_size(1280, 768)
    win.set_size_request(800, 600)
    win.set_wmclass("shilploka", "ShilpLoka")
    icon = os.path.join(SNAP, "meta", "gui", "shilploka.png") if SNAP else ""
    if icon and os.path.exists(icon):
        win.set_icon_from_file(icon)

    view = build_webview()
    win.add(view)

    # F11 toggles fullscreen, like most desktop games.
    state = {"full": False}

    def on_key(_w, event):
        if event.keyval == Gdk.KEY_F11:
            if state["full"]:
                win.unfullscreen()
            else:
                win.fullscreen()
            state["full"] = not state["full"]
            return True
        return False

    win.connect("key-press-event", on_key)
    win.connect("destroy", Gtk.main_quit)

    view.load_uri("file://" + os.path.abspath(GAME_HTML))
    win.show_all()
    Gtk.main()
    return 0


if __name__ == "__main__":
    sys.exit(main())
