# Own presentation state by user workflow

Phone presentation state is owned by onboarding, recording, and settings workflows rather than one application-wide MVI contract. The recording workflow spans selection, details, preview, setup, permissions, active recording, and annotations because those routes edit or observe one recording request.

The application shell owns navigation and a small read-only view of recording session facts. Routes remain stateless and receive focused state and intents from their owning feature.
