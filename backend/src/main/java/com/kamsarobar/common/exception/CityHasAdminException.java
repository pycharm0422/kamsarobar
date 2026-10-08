package com.kamsarobar.common.exception;

/**
 * The target city already has an admin. The client may repeat the request with replaceExistingAdmin=true
 * to make the new person the admin (the current one becomes a regular member).
 */
public class CityHasAdminException extends ConflictException {

    public static final String CODE = "CITY_HAS_ADMIN";

    public CityHasAdminException(String cityName, String currentAdmins) {
        super(cityName + " already has an admin: " + currentAdmins + ".");
    }
}
