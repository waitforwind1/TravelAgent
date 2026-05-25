import type { BaseResponse, TravelExportResponse, TravelPlanRequest, TravelPlanResponse } from './types'

const API_BASE = '/api'

async function parseJsonResponse<T>(response: Response): Promise<BaseResponse<T>> {
  const payload = (await response.json()) as BaseResponse<T>

  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.description || payload.msg || `Request failed: ${response.status}`)
  }

  if (!payload.data) {
    throw new Error(payload.description || 'No data returned from server')
  }

  return payload
}

export async function postJson<T>(path: string, body: TravelPlanRequest): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=utf-8',
    },
    body: JSON.stringify(body),
  })

  const payload = await parseJsonResponse<T>(response)
  return payload.data as T
}

export function generatePlan(body: TravelPlanRequest) {
  return postJson<TravelPlanResponse>('/travel/plan', body)
}

export function continueChat(body: TravelPlanRequest) {
  return postJson<TravelPlanResponse>('/travel/chat', body)
}

export function ragChat(body: TravelPlanRequest) {
  return postJson<TravelPlanResponse>('/travel/chatWithRag', body)
}

export function exportPlan(body: TravelPlanRequest) {
  return postJson<TravelExportResponse>('/travel/plan/generateAndExport', body)
}

function handleStreamData(
  data: string,
  onToken: (token: string) => void,
  onConversationId: (conversationId: string) => void,
) {
  if (!data) return

  if (data.startsWith('conversationId:')) {
    onConversationId(data.replace('conversationId:', '').trim())
    return
  }

  onToken(data)
}

function parseStreamEvent(chunk: string) {
  return chunk
    .split(/\r?\n/)
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.replace(/^data: ?/, ''))
    .join('\n')
}

export async function streamPlan(
  body: TravelPlanRequest,
  onToken: (token: string) => void,
  onConversationId: (conversationId: string) => void,
) {
  const response = await fetch(`${API_BASE}/travel/plan/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=utf-8',
    },
    body: JSON.stringify(body),
  })

  if (!response.ok || !response.body) {
    let message = `Stream request failed: ${response.status}`
    try {
      const payload = (await response.json()) as BaseResponse<null>
      message = payload.description || payload.msg || message
    } catch {
      // Keep the HTTP message when the server did not return JSON.
    }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split(/\n\n/)
    buffer = chunks.pop() || ''

    for (const chunk of chunks) {
      handleStreamData(parseStreamEvent(chunk), onToken, onConversationId)
    }
  }

  if (buffer.trim()) {
    handleStreamData(parseStreamEvent(buffer), onToken, onConversationId)
  }
}
