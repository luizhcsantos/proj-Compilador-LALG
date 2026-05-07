<!-- Project README: quick environment + run instructions -->

# proj-Compilador-LALG — Quick start

This repository is a small compiler project with a Spring Boot backend and a Vue 3 + Vite frontend.

## Required runtimes
- Java (JDK): pom.xml currently sets maven.compiler.source/target = 25 and Spring Boot 4.0.0. If you cannot install JDK 25 on your machine, use a compatible JDK (recommended: JDK 21 LTS). To avoid compiler issues you can either install a matching JDK or edit `pom.xml` and lower `<maven.compiler.source>`/`<maven.compiler.target>` to your installed JDK (for example `21` or `17`).
- Node: `frontend-compilador/package.json` requires Node ^20.19.0 or >=22.12.0. Use Node 20.19.x or any >=22.12.0.

## Quick commands (PowerShell)

Check Java/Maven versions:
```powershell
mvn -v
java -version
```

Development boot order (open two terminals):
```powershell
# Terminal A: backend (from repo root)
mvn -q -DskipTests compile
mvn -DskipTests spring-boot:run

# Terminal B: frontend
Set-Location "frontend-compilador"
npm install
npm run dev
```

Build for production:
```powershell
# Build backend
mvn -q -DskipTests package

# Build frontend (type-check + build)
Set-Location "frontend-compilador"
npm install
npm run build
```

## Common troubleshooting
- Frontend type-check can fail with TS7016 for `./App.vue`. Fix by adding a Vue module shim to `frontend-compilador/env.d.ts` (example: add a line `declare module '*.vue';`) or add a `shims-vue.d.ts` file.
- If Maven compilation fails due to Java version, edit `pom.xml` properties `maven.compiler.source`/`maven.compiler.target` to match your JDK.

--
For more detailed contributor notes, see `AGENTS.md` in the project root.


