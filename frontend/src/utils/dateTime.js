const parseLocalDateTime = (value) => {
  if (typeof value !== 'string') return null;

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/);
  if (!match) return null;

  const [, year, month, day, hour, minute, second = '00'] = match;
  return { year, month, day, hour, minute, second };
};

export const formatLocalDateTime = (value) => {
  const parts = parseLocalDateTime(value);
  if (!parts) return value || '';

  return `${parts.hour}:${parts.minute}:${parts.second} ${Number(parts.day)}/${Number(parts.month)}/${parts.year}`;
};

export const formatLocalDate = (value) => {
  const parts = parseLocalDateTime(value);
  if (!parts) return value || '';

  return `${Number(parts.day)}/${Number(parts.month)}/${parts.year}`;
};