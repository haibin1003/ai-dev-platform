import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HighlightedText from '../HighlightedText.vue'

describe('HighlightedText', () => {
  it('should render text without highlight when keyword is empty', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'Hello World',
        keyword: ''
      }
    })

    expect(wrapper.text()).toBe('Hello World')
    expect(wrapper.find('mark').exists()).toBe(false)
  })

  it('should highlight matching keyword', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'Hello World',
        keyword: 'World'
      }
    })

    const mark = wrapper.find('mark')
    expect(mark.exists()).toBe(true)
    expect(mark.text()).toBe('World')
  })

  it('should be case insensitive', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'Hello WORLD',
        keyword: 'world'
      }
    })

    const mark = wrapper.find('mark')
    expect(mark.exists()).toBe(true)
    expect(mark.text()).toBe('WORLD')
  })

  it('should highlight multiple occurrences', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'test test test',
        keyword: 'test'
      }
    })

    const marks = wrapper.findAll('mark')
    expect(marks).toHaveLength(3)
  })

  it('should not use v-html (security)', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: '<script>alert("xss")</script>',
        keyword: 'xss'
      }
    })

    // Should escape HTML tags, not render them
    expect(wrapper.html()).not.toContain('<script>')
    expect(wrapper.text()).toContain('<script>')
  })

  it('should handle partial matches correctly', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'testing tester test',
        keyword: 'test'
      }
    })

    const marks = wrapper.findAll('mark')
    // Should match "test" at start of "testing" and "tester" and standalone "test"
    expect(marks.length).toBeGreaterThanOrEqual(3)
  })

  it('should handle special characters in text', () => {
    const wrapper = mount(HighlightedText, {
      props: {
        text: 'Error: [2024-01-01] Something failed!',
        keyword: '[2024'
      }
    })

    const mark = wrapper.find('mark')
    expect(mark.exists()).toBe(true)
    expect(mark.text()).toBe('[2024')
  })
})
