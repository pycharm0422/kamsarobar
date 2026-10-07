/** Turns an API error into a readable message (same rules as the website). */
export function errorMessage(error) {
  const body = error?.response?.data;
  if (body?.fieldErrors) return Object.values(body.fieldErrors).join('. ');
  if (body?.message) return body.message;
  if (error?.code === 'ECONNABORTED' || !error?.response) {
    return error?.message && !error.message.includes('Network') ? error.message : 'Cannot reach the server. Check your internet connection.';
  }
  return 'Something went wrong. Please try again.';
}
