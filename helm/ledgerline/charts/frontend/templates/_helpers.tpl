{{- define "frontend.fullname" -}}
ledgerline-frontend
{{- end }}

{{- define "frontend.labels" -}}
app: ledgerline-frontend
app.kubernetes.io/part-of: ledgerline
{{- end }}

{{- define "frontend.selectorLabels" -}}
app: ledgerline-frontend
{{- end }}

{{- define "frontend.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
