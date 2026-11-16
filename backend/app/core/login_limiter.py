from collections import defaultdict, deque
from datetime import datetime, timedelta, timezone
from threading import Lock

_attempts: dict[str, deque[datetime]] = defaultdict(deque)
_lock = Lock()


def allow_login_attempt(key: str, limit: int, window_seconds: int = 60) -> bool:
    now = datetime.now(timezone.utc)
    cutoff = now - timedelta(seconds=window_seconds)
    with _lock:
        attempts = _attempts[key]
        while attempts and attempts[0] < cutoff:
            attempts.popleft()
        if len(attempts) >= limit:
            return False
        attempts.append(now)
        return True
