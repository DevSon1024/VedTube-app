1. Never skip tests.
2. Never move to the next phase if the current phase fails.
3. Never put network calls inside Composables.
4. Never create ExoPlayer instances inside Composables.
5. Never store temporary signed playback URLs permanently.
6. Never use GlobalScope.
7. Never use destructive Room migrations.
8. Never add fake functionality.
9. Never introduce a dependency without checking its license and maintenance status.
10. Never modify unrelated features while fixing a phase.
11. Run ./gradlew test after significant changes.
12. Run ./gradlew assembleDebug before declaring a phase complete.
13. Keep provider/extraction code isolated from UI.
14. Keep YouTube-specific code behind provider interfaces.
15. Prefer fixing architecture over adding workarounds.
