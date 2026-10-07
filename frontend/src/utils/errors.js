/** Turns an API error into a human-readable message (and field errors, if any). */
export function errorMessage(error) {
  const body = error?.response?.data;
  if (body?.fieldErrors) {
    return Object.values(body.fieldErrors).join('. ');
  }
  if (body?.message) {
    return body.message;
  }
  if (error?.code === 'ECONNABORTED' || !error?.response) {
    return 'Cannot reach the server. Please check your connection.';
  }
  return 'Something went wrong. Please try again.';
}
