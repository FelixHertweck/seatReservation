export const customSerializer = {
  json: <T>(body: T): string => JSON.stringify(body, null, 2),
};
