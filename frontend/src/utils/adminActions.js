/**
 * A city has one admin. If the server says the target city already has one (CITY_HAS_ADMIN), ask the main admin
 * whether to replace them, and repeat the request with replaceExistingAdmin=true if they agree.
 * Returns the result, or null if the main admin chose not to replace.
 */
export async function withAdminReplacement(request, newAdminName) {
  try {
    return await request(false);
  } catch (err) {
    if (err?.response?.data?.code !== 'CITY_HAS_ADMIN') throw err;
    const ok = window.confirm(
      `${err.response.data.message}\n\nMake ${newAdminName} the admin instead? The current admin will become a regular member.`,
    );
    return ok ? request(true) : null;
  }
}
