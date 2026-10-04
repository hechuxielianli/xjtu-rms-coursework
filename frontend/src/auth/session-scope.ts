const clearers = new Set<() => void>();
/** Future bounded query stores/forms register explicit reset hooks; no persistence plugin. */
export function registerSessionClearer(clearer: () => void): () => void {
  clearers.add(clearer);
  return () => { clearers.delete(clearer); };
}
export function clearSessionScope(): void {
  for (const clearer of [...clearers]) clearer();
}
