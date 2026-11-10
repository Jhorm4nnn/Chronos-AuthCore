package com.pamplona.authcore.domain;

/**
 * Regla de HU-1: la contrasena tiene al menos 8 caracteres e incluye letras y numeros.
 * Vive en el dominio para que el registro (REST) y el administrador inicial apliquen la misma regla.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String DESCRIPCION = "debe tener al menos 8 caracteres e incluir letras y numeros";

    private PasswordPolicy() {
    }

    public static boolean cumple(String password) {
        return password != null && password.matches(REGEX);
    }
}
