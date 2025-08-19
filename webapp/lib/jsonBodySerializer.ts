export const customSerializer = {
  json: <T>(body: T): string =>
    JSON.stringify(
      body,
      (_key, value) => (typeof value === "bigint" ? value.toString() : value),
      2,
    ),
};
