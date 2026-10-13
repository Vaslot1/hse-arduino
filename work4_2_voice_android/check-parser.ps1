$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$workspaceRoot = Split-Path $taskRoot -Parent
$javaBin = Join-Path $workspaceRoot 'tools\android-studio\jbr\bin'
if (!(Test-Path -LiteralPath (Join-Path $javaBin 'javac.exe'))) {
    if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK.' }
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
}
$outputDir = Join-Path $taskRoot 'build\parser-check'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$sourceRoot = Join-Path $taskRoot 'app\src\main\java\com\example\voicehome'
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -d $outputDir (Join-Path $sourceRoot 'CommandParser.java') (Join-Path $sourceRoot 'SpeechResponses.java') (Join-Path $taskRoot 'tests\CommandParserTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Parser compilation failed.' }
& (Join-Path $javaBin 'java.exe') -cp $outputDir com.example.voicehome.CommandParserTest
if ($LASTEXITCODE -ne 0) { throw 'Parser tests failed.' }
