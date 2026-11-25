import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createImportAttempts } from '../src/importAttempts.js'

test('only an unchanged upload group reuses the failed request key', () => {
  let sequence = 0
  const attempts = createImportAttempts(() => `attempt-${++sequence}`)
  const first = { uploadId: 'file-1' }
  const second = { uploadId: 'file-2' }
  const original = attempts.keyFor('batch-1', 'TEXT', [first])
  assert.equal(attempts.keyFor('batch-1', 'TEXT', [first]), original)
  const expanded = attempts.keyFor('batch-1', 'TEXT', [first, second])
  assert.notEqual(expanded, original)
  assert.equal(attempts.keyFor('batch-1', 'TEXT', [first, second]), expanded)
  assert.notEqual(attempts.keyFor('batch-1', 'TEXT', [second]), expanded)
  assert.notEqual(attempts.keyFor('batch-2', 'TEXT', [first]), original)
  assert.notEqual(attempts.keyFor('batch-1', 'IMAGE', [first]), original)
})
