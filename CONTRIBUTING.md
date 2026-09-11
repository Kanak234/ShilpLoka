# Contributing to ShilpLoka (शिल्पलोक)

Thank you for your interest in contributing to ShilpLoka, an ancient Indian voxel sandbox and cultural civilization engine.

## Code of Conduct

All contributors are expected to uphold a respectful, welcoming, and collaborative environment.

## Development Workflow

### 1. Prerequisites
- **Node.js**: v20 or higher
- **npm**: v10 or higher
- **Docker** (optional, for containerized smoke tests)

### 2. Setup
```bash
git clone https://github.com/Kanak234/ShilpLoka.git
cd ShilpLoka
npm ci
```

### 3. Local Development & Testing
```bash
# Start local development server
npm run dev

# Run Vitest test suite
npm test

# Run tests with strict v8 coverage gate (>=80%)
npm run test:coverage

# Build deployable single-file bundle
npm run build
```

### 4. Coding Standards
- **Zero Mock Data**: Implement authentic game logic and data structures without synthetic shortcuts.
- **Test Coverage**: All new features and domain logic must maintain test coverage >= 80% across statements, branches, functions, and lines.
- **Architectural Separation**: Decouple domain algorithms (such as `ShilpGraph`, `VastuGrid`, and `KalaChakra`) from direct DOM and browser APIs.
- **Security**: Run `npm audit` and ensure all dependencies remain vulnerability-free.

## Pull Request Guidelines

1. Create a descriptive feature branch from `main`.
2. Ensure `npm run test:coverage` and `npm run build` pass cleanly before committing.
3. Open a Pull Request referencing relevant issues and describing architectural decisions.
