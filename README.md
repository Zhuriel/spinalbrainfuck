#spinal brainfuck

the most metal processor name. yes i made this just for that joke.

to run tests: `sbt test`

to run any brainfuck program: `sbt "runMain SpinalBrainFuckInterpreter <program file> [<inputs>]`
(interactive mode coming eventually maybe)

to generate vhdl: `sbt "runMain SpinalBrainFuck"`
(actual hardware demonstration coming eventually maybe)

hardware description (all files in `src/main`) licensed under wtfpl
brainfuck test code by daniel b cristofani from brainfuck.org licensed under CC BY-SA 4.0