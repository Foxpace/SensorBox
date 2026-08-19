# Use explicit application outcomes

Expected failures crossing repository, storage, protocol, recording, use-case, or presentation interfaces use a shared `AppResult<T>` and data-only `AppError`. Kotlin `Result<T>` and custom exceptions do not cross those interfaces because callers need stable error codes, exhaustive handling, and diagnostics without constructor side effects.

Infrastructure adapters translate expected exceptions and preserve safe causes. Application entry points record terminal failures through an injected local diagnostic logger. User-facing messages remain presentation decisions.
