# DirectCall - Database
This directory will contain the SQLite schema and logic for the application.
Since the application operates 100% offline, all user accounts, contacts, and call history are stored locally.

Upcoming tables:
- `users`: Local user profile (ID, Name, Phone, Password Hash)
- `contacts`: Saved peers (ID, Name, Alias)
- `call_history`: Logs of offline calls (Peer ID, Timestamp, Duration, Type)
