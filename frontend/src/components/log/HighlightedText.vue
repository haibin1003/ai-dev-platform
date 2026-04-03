<template>
  <span class="highlighted-text">
    <template v-for="(part, index) in parts" :key="index">
      <mark v-if="part.isMatch" class="highlight-mark">{{ part.text }}</mark>
      <template v-else>{{ part.text }}</template>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
  keyword: string
}>()

interface TextPart {
  text: string
  isMatch: boolean
}

const parts = computed((): TextPart[] => {
  if (!props.keyword.trim()) {
    return [{ text: props.text, isMatch: false }]
  }

  const keyword = props.keyword
  const result: TextPart[] = []
  let remaining = props.text

  while (remaining) {
    const index = remaining.toLowerCase().indexOf(keyword.toLowerCase())
    if (index === -1) {
      result.push({ text: remaining, isMatch: false })
      break
    }

    if (index > 0) {
      result.push({ text: remaining.slice(0, index), isMatch: false })
    }

    result.push({ text: remaining.slice(index, index + keyword.length), isMatch: true })
    remaining = remaining.slice(index + keyword.length)
  }

  return result
})
</script>

<style scoped>
.highlighted-text {
  white-space: pre-wrap;
  word-break: break-all;
}

.highlight-mark {
  background: #e6a23c;
  color: #000;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
