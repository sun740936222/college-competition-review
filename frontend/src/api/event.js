import api from './index'

export function listEvents() {
  return api.get('/events')
}

export function getEvent(id) {
  return api.get(`/events/${id}`)
}
