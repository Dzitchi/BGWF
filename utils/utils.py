from transliterate import translit


def safe_translit(text):
    """Транслитерация текста с автоматическим определением языка."""
    if len(text) <= 1:
        return text
    try:
        if any("а" <= c <= "я" for c in text.lower()):
            return translit(text, reversed=True)
        else:
            return translit(text, "ru")
    except ValueError as e:
        print(f"Transliteration failed: {e}")
        return text
