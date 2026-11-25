export function createImportAttempts(newKey = () => crypto.randomUUID()) {
  const attempts = new Map()
  return {
    keyFor(batchId, sourceType, entries) {
      const scope = JSON.stringify([batchId, sourceType])
      const members = JSON.stringify(entries.map((entry) => entry.uploadId))
      const previous = attempts.get(scope)
      if (previous?.members === members) return previous.key
      const key = newKey()
      attempts.set(scope, { members, key })
      return key
    },
  }
}
