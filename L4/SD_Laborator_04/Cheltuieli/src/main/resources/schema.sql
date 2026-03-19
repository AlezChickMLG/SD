CREATE TABLE IF NOT EXISTS account (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        hashedUsername TEXT NOT NULL UNIQUE,
        encryptedUsername TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL,
        salt TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS buget (
    hashedUsername TEXT PRIMARY KEY,
    bugetTotal REAL,
    intretinere REAL,
    mancare REAL,
    distractie REAL,
    scoala REAL,
    personale REAL,
    FOREIGN KEY (hashedUsername) REFERENCES account(hashedUsername)
);

PRAGMA foreign_keys = ON;