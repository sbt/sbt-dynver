onLoad in Global := (onLoad in Global).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
