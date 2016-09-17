package sbtdynver

import java.util.Date

final case class FakeClock(now: Date) extends Clock
