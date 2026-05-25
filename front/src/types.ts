export interface TravelPlanRequest {
  message?: string
  conversationId?: string
  city?: string
  startLocation?: string
  destinationArea?: string
  dateTime?: string
  budget?: string
  people?: string
  preference?: string
}

export interface BaseResponse<T> {
  data: T | null
  code: number
  msg: string
  description: string
}

export interface TravelPlanResponse {
  conversationId: string
  content: string
}

export interface TravelExportResponse {
  conversationId: string
  content: string
  filePath: string
}

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  time: string
}

export type RequestMode = 'idle' | 'generating' | 'streaming' | 'chatting' | 'rag' | 'exporting'
