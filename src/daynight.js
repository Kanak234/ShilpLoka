/**
 * ============================================================================
 * DAY-NIGHT CYCLE & CELESTIAL LIGHTING
 * ============================================================================
 * Simulates a continuous celestial day-night cycle with orbiting Sun and Moon,
 * dynamic atmospheric sky color interpolation (Dawn, Day, Dusk, Night),
 * a twinkling starfield that fades with solar elevation, and ambient lighting.
 */

import * as THREE from 'three';

export class DayNightCycle {
  /**
   * @param {THREE.Scene} scene
   * @param {number} dayDurationSeconds - Real-world seconds for a full 24-hour in-game cycle
   */
  constructor(scene, dayDurationSeconds = 180) {
    this.scene = scene;
    this.dayDuration = dayDurationSeconds;
    this.time = 0.45; // 0.0 = midnight, 0.25 = sunrise, 0.45-0.5 = bright daytime noon, 0.75 = sunset
    this.timeSpeed = 1.0;

    // Atmospheric Colors
    this.skyColors = {
      midnight: new THREE.Color(0x050814),
      dawn: new THREE.Color(0xf39c12),
      noon: new THREE.Color(0x78a7ff),
      dusk: new THREE.Color(0xb83227),
    };

    this.initLighting();
    this.initCelestials();
    this.initStarfield();
  }

  initLighting() {
    // Universal ambient light ensuring all voxel faces are clear and visible
    this.ambientLight = new THREE.AmbientLight(0xffffff, 0.85);
    this.scene.add(this.ambientLight);

    // Hemisphere ambient light (sky color / ground color)
    this.hemiLight = new THREE.HemisphereLight(0x78a7ff, 0x3d352a, 0.6);
    this.scene.add(this.hemiLight);

    // Directional celestial sun light
    this.sunLight = new THREE.DirectionalLight(0xfff8e7, 1.2);
    this.sunLight.castShadow = false; // Fast voxel shading
    this.scene.add(this.sunLight);

    // Cool blue moonlight
    this.moonLight = new THREE.DirectionalLight(0x5c7aff, 0.2);
    this.scene.add(this.moonLight);
  }

  initCelestials() {
    // Sun Mesh (bright glowing yellow square billboard)
    const sunGeom = new THREE.PlaneGeometry(24, 24);
    const sunMat = new THREE.MeshBasicMaterial({
      color: 0xfff0aa,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.95,
      depthWrite: false,
    });
    this.sunMesh = new THREE.Mesh(sunGeom, sunMat);
    this.scene.add(this.sunMesh);

    // Moon Mesh (silvery pale white square billboard)
    const moonGeom = new THREE.PlaneGeometry(18, 18);
    const moonMat = new THREE.MeshBasicMaterial({
      color: 0xdfe6e9,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.9,
      depthWrite: false,
    });
    this.moonMesh = new THREE.Mesh(moonGeom, moonMat);
    this.scene.add(this.moonMesh);
  }

  initStarfield() {
    const starCount = 800;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(starCount * 3);

    for (let i = 0; i < starCount; i++) {
      // Distribute stars uniformly over upper celestial dome
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(Math.random() * 0.9 + 0.1);
      const r = 380;

      positions[i * 3 + 0] = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.cos(phi);
      positions[i * 3 + 2] = r * Math.sin(phi) * Math.sin(theta);
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    this.starMaterial = new THREE.PointsMaterial({
      color: 0xffffff,
      size: 2.0,
      transparent: true,
      opacity: 0.0,
      depthWrite: false,
    });
    this.stars = new THREE.Points(geometry, this.starMaterial);
    this.scene.add(this.stars);
  }

  /**
   * Evaluates atmospheric sky color based on solar elevation angle
   */
  getSkyColor() {
    const t = this.time; // [0, 1]
    const color = new THREE.Color();

    if (t < 0.2) {
      // Midnight to Pre-dawn
      color.copy(this.skyColors.midnight);
    } else if (t < 0.3) {
      // Dawn transition (midnight -> dawn -> noon)
      const factor = (t - 0.2) / 0.1;
      if (factor < 0.5) {
        color.lerpColors(this.skyColors.midnight, this.skyColors.dawn, factor * 2);
      } else {
        color.lerpColors(this.skyColors.dawn, this.skyColors.noon, (factor - 0.5) * 2);
      }
    } else if (t < 0.7) {
      // High Daytime
      color.copy(this.skyColors.noon);
    } else if (t < 0.8) {
      // Dusk transition (noon -> dusk -> midnight)
      const factor = (t - 0.7) / 0.1;
      if (factor < 0.5) {
        color.lerpColors(this.skyColors.noon, this.skyColors.dusk, factor * 2);
      } else {
        color.lerpColors(this.skyColors.dusk, this.skyColors.midnight, (factor - 0.5) * 2);
      }
    } else {
      // Post-dusk into Midnight
      color.copy(this.skyColors.midnight);
    }

    return color;
  }

  /**
   * Updates sun/moon positions, lighting intensities, star visibility, and sky background
   */
  update(dt, playerPosition) {
    // Advance continuous time
    this.time = (this.time + (dt / this.dayDuration) * this.timeSpeed) % 1.0;

    // Celestial orbital angle: 0 rad at morning horizon, PI/2 at high noon
    const angle = (this.time - 0.25) * Math.PI * 2;
    const orbitRadius = 350;

    const sunX = playerPosition.x + Math.cos(angle) * orbitRadius;
    const sunY = playerPosition.y + Math.sin(angle) * orbitRadius;
    const sunZ = playerPosition.z + Math.sin(angle * 0.3) * 60; // slight orbital tilt

    const moonX = playerPosition.x - Math.cos(angle) * orbitRadius;
    const moonY = playerPosition.y - Math.sin(angle) * orbitRadius;
    const moonZ = playerPosition.z - Math.sin(angle * 0.3) * 60;

    // Position sun and moon
    this.sunMesh.position.set(sunX, sunY, sunZ);
    this.sunMesh.lookAt(playerPosition.x, playerPosition.y, playerPosition.z);

    this.moonMesh.position.set(moonX, moonY, moonZ);
    this.moonMesh.lookAt(playerPosition.x, playerPosition.y, playerPosition.z);

    // Position directional lights
    this.sunLight.position.set(sunX, sunY, sunZ);
    this.sunLight.target.position.copy(playerPosition);
    this.sunLight.target.updateMatrixWorld();

    this.moonLight.position.set(moonX, moonY, moonZ);
    this.moonLight.target.position.copy(playerPosition);
    this.moonLight.target.updateMatrixWorld();

    // Center starfield dome around player
    this.stars.position.copy(playerPosition);

    // Calculate solar elevation factor: > 0 means sun is above horizon
    const elevation = Math.sin(angle);
    const dayFactor = Math.max(0.0, Math.min(1.0, elevation * 2.5));

    // Dynamic light intensities
    this.ambientLight.intensity = 0.55 + dayFactor * 0.45; // 0.55 at night, 1.0 during bright day
    this.sunLight.intensity = Math.max(0.0, elevation) * 1.3;
    this.moonLight.intensity = Math.max(0.0, -elevation) * 0.35;
    this.hemiLight.intensity = 0.35 + dayFactor * 0.5;

    // Star opacity increases as daylight wanes
    const nightFactor = Math.max(0.0, Math.min(1.0, -elevation * 2.0));
    this.starMaterial.opacity = nightFactor * 0.85;

    // Dynamic sky background color
    const currentSky = this.getSkyColor();
    this.scene.background = currentSky;
    this.scene.fog = new THREE.FogExp2(currentSky.getHex(), 0.012);
  }

  getTimeFormatted() {
    const totalMinutes = Math.floor(this.time * 24 * 60);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    const ampm = hours >= 12 ? 'PM' : 'AM';
    const displayHours = ((hours + 11) % 12 + 1);
    const padM = minutes < 10 ? '0' + minutes : minutes;
    return `${displayHours}:${padM} ${ampm}`;
  }
}
