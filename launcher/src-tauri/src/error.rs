use serde::{Serialize, Serializer};

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("{0}")]
    Io(#[from] std::io::Error),

    #[error("{0}")]
    Json(#[from] serde_json::Error),

    #[error("network: {0}")]
    Http(#[from] reqwest::Error),

    #[error("{0} not found")]
    NotFound(String),

    #[error("{0}")]
    Invalid(String),

    /// A surface that exists in the contract but has no core behind it yet.
    /// Reported as-is rather than answered with a plausible-looking lie —
    /// a launcher that fakes success is worse than one that admits a gap.
    #[error("not implemented yet: {0}")]
    Unimplemented(&'static str),
}

pub type Result<T> = std::result::Result<T, Error>;

impl Serialize for Error {
    fn serialize<S: Serializer>(&self, s: S) -> std::result::Result<S::Ok, S::Error> {
        s.serialize_str(&self.to_string())
    }
}
