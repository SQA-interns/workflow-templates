# Firebase Auth emulator for the local stack and tests only; never deployed (D-29, D-32).
FROM node:22.23.3-bookworm-slim
RUN npm install -g firebase-tools@14.20.0 --no-audit --no-fund && npm cache clean --force
WORKDIR /emulator
COPY --chown=node:node firebase.json /emulator/firebase.json
USER node
EXPOSE 9099
CMD ["firebase", "emulators:start", "--only", "auth", "--project", "demo-contacts"]
