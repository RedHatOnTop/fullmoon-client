use md5::{Digest, Md5};

use crate::{
    error::{Error, Result},
    model::Account,
};

pub fn create_account(username: &str) -> Result<Account> {
    let name = username.trim();
    if name.is_empty()
        || name.len() > 16
        || !name
            .chars()
            .all(|character| character.is_ascii_alphanumeric() || character == '_')
    {
        return Err(Error::Invalid(
            "a Minecraft name is 1-16 characters of letters, digits or underscore".into(),
        ));
    }

    let digest = Md5::digest(format!("OfflinePlayer:{name}").as_bytes());
    let mut bytes = [0; 16];
    bytes.copy_from_slice(&digest);
    let uuid = uuid::Builder::from_md5_bytes(bytes).into_uuid();

    Ok(Account {
        uuid: uuid.to_string(),
        username: name.into(),
        skin_hue: (uuid.as_u128() % 360) as u16,
        skin_url: None,
        source: "offline".into(),
        capes: Vec::new(),
    })
}

pub fn append_account(accounts: Vec<Account>, account: Account) -> Result<Vec<Account>> {
    if accounts
        .iter()
        .any(|existing| existing.uuid == account.uuid)
    {
        return Err(Error::Invalid(format!(
            "{} is already added",
            account.username
        )));
    }

    Ok(accounts
        .into_iter()
        .chain(std::iter::once(account))
        .collect())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn creates_the_vanilla_offline_identity_from_a_trimmed_name() {
        let account = create_account("  FullmoonTest  ").unwrap();

        assert_eq!(account.username, "FullmoonTest");
        assert_eq!(account.uuid, "3ab0b1ff-2b79-3b6f-a626-096ec31f8fff");
        assert_eq!(account.source, "offline");
        assert!(account.skin_url.is_none());
        assert!(account.capes.is_empty());
    }

    #[test]
    fn rejects_names_minecraft_cannot_use() {
        for name in ["", "more_than_16_chars", "moon-light", "달빛"] {
            assert!(create_account(name).is_err(), "accepted {name:?}");
        }
    }

    #[test]
    fn appends_without_changing_existing_accounts() {
        let existing = create_account("Existing").unwrap();
        let added = create_account("FullmoonTest").unwrap();
        let accounts = append_account(vec![existing.clone()], added.clone()).unwrap();

        assert_eq!(accounts.len(), 2);
        assert_eq!(accounts[0].uuid, existing.uuid);
        assert_eq!(accounts[1].uuid, added.uuid);
    }

    #[test]
    fn refuses_the_same_offline_identity_twice() {
        let account = create_account("FullmoonTest").unwrap();
        let error = append_account(vec![account.clone()], account)
            .unwrap_err()
            .to_string();

        assert_eq!(error, "FullmoonTest is already added");
    }
}
