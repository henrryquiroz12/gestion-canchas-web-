package com.canchas.web.validation;

/** Rules shared by Bean Validation and the account form templates. */
public final class AccountRules {
    private AccountRules() {}

    public static final String NAME = "^[\\p{L}][\\p{L}\\p{M}]*(?: +[\\p{L}][\\p{L}\\p{M}]*)*$";
    public static final String NAME_MESSAGE = "Usa solo letras y espacios entre palabras";
    public static final String DNI = "^[0-9]{8}$";
    public static final String OPTIONAL_DNI = "^$|" + DNI;
    public static final String PHONE = "^9[0-9]{8}$";
    public static final String EMAIL = "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:gmail\\.com|hotmail\\.com|utp\\.edu\\.pe)$";
    public static final String EMAIL_MESSAGE = "Ingresa un correo válido de gmail.com, hotmail.com o utp.edu.pe, sin espacios";
}
