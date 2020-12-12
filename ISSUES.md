# Why was my issue closed?

This document goes over some common causes for issue closures.

## You did not fill in the template

I can't debug the problem unless you provide the information the template asks for. 
If you cannot put in the effort to fill out a template, then don't expect me to put in the effort to fix it.

## Your issue was already reported

If the problem in your issue has already been encountered before, it will be closed - especially if your report doesn't provide any new information.
Make sure you search the *closed* issues before making an issue.

I do not link the specific issue(s) that report the same problem, because searching takes time - if you're interested in finding them, you should be able to do so without my help.

To be clear: I do **not** expect users to look at *all* previous issues, or do a comprehensive stack trace analysis to see if their crash was already reported.

## Your issue was already fixed

The problem you reported has been addressed. Note that this does **not** mean that the latest stable version of Mindustry has the fix! 
It simply means that I have committed (or am about to commit) a patch that fixes it *on the current development branch*.

## Your issue is missing a crash report or log

If the game crashes without a specific cause, and you don't send me a log, I can't fix it. There is no way for me to know what went wrong.

During a normal crash, the game should tell you where the log is saved. If not, you should still be able to look in the game's crash folder on most operating systems, or export the logs in *Settings -> Game Data -> Export Crash Logs*.

## Your issue is missing saves or screenshots

Even if you think your problem happens everywhere and saves/screenshots are redundant, this is frequently not the case.
If I cannot reproduce the problem on my own saves and you have not linked any of your own, then the problem is likely to be save-specific. If you do not send me any, the problem cannot be investigated further.

## Your issue is related to an external program

If Mindustry causes something else to crash or misbehave, I am very unlikely to fix it. Unless the problem is serious, widespread and/or clearly a bug *in Mindustry*, it is not my responsibility.

Similarly, if you use another (invasive) program to change how Mindustry works, and something goes wrong, that is not my problem. Don't do it.

## Your issue is caused by mods

Crashes and bugs related to installed mods should be reported in the relevant mod repository, not here.
*Note that problems with the Mindustry modding API are a separate problem, and do not apply.*

## I cannot reproduce your issue

If I follow your instructions and am repeatedly unable to reproduce the problem you've reported, then it is very unlikely to be fixed. 
Either the problem is device-specific, or there is not enough information given for me to be able to reproduce it.

I may attempt to change some code if I think it will make the issue less likely to occur, but without knowing for sure, the issue cannot be considered truly "fixed". 
As I cannot make any further progress on the problem, there is no reason to keep it open. If it is a common bug/crash, other people will come along with information that may shed some light on the issue.
