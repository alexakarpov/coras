# EPLK

Granted, a silly name - but this was me learning core.async, which I've neglected all this time. It's relatively new - back in 2014-15 we didn't use in SparX..

## Installation

as long as you have Clojure and leiningen installed, *lein deps* and *lein compile* should take care of everything

## Usage

### To run with provided resources/events.in:

**make clean** ; **make compile** ; **make run** ; **make check**

, but that's boring as hell - and, reading events from STDIN won't test the timeouts functionality! The fun part is to run it interactively. Emacs with **CIDER** is recommended - *core.clj* has all the commands to play/evaluate.

## Examples

the simplest interactive session looks like this:

```
> lein with-profile dev repl
...
eplk.core=>
```

From here:

```
eplk.core=> (utils/report-on-chan @in-ch) ; explore channel state
"channel size: 0, closed?: false"
eplk.core=> (driver/toggle) ; toggle switch for the event-processing machine on/off
false
eplk.core=> (driver/toggle)
true
;; note that at this point machine is "unpowered", so the events won't be processed yet, though the channel is ready to receive them
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC") ;; channel is buffered at 10, so more submits will be rejected
[:error :channel_full]

eplk.core=> (utils/report-on-chan @in-ch) ; now channel has 10 messages queued
"channel size: 10, closed?: false"
eplk.core=> (start) ;; the machine is on!
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x2b0171e3 "clojure.core.async.impl.channels.ManyToManyChannel@2b0171e3"]
eplk.core=> (submit-event "ABC")
true
eplk.core=> (submit-event "ABC")
true
eplk.core=> (utils/report-on-chan @in-ch)
"channel size: 0, closed?: false" ; size is 0, because all the events above have already been consumed by the machine. *tail -f /tmp/journal.out* for detail. Wait for 45+15 seconds to observe timeout-related messages:
...
{"type":"MachineCycled","recorded_at":"2018-01-30T04:40:21Z","machine_id":"ABC","timestamp":"2018-01-30T04:40:07Z"}
{"type":"MachineCycled","recorded_at":"2018-01-30T04:40:48Z","machine_id":"ABC","timestamp":"2018-01-30T04:40:48Z"}
{"type":"MachineCycled","recorded_at":"2018-01-30T04:40:48Z","machine_id":"ABC","timestamp":"2018-01-30T04:40:48Z"}
{"type":"MachineCycled","recorded_at":"2018-01-30T04:40:49Z","machine_id":"ABC","timestamp":"2018-01-30T04:40:49Z"}
{"type":"MachineCycled","recorded_at":"2018-01-30T04:40:56Z","machine_id":"ABC","timestamp":"2018-01-30T04:40:56Z"}
{:type "NonProductionLimitReached", :machine_id nil, :timestamp "2018-01-30T04:41:41Z"}
{"type":"MachineCycled","recorded_at":"2018-01-30T04:41:54Z","machine_id":"ABC","timestamp":"2018-01-30T04:41:54Z"}
{:type "NonProductionLimitReached", :machine_id nil, :timestamp "2018-01-30T04:42:39Z"}
{:type "AlarmOpened", :machine_id nil, :timestamp "2018-01-30T04:42:54Z"}

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
