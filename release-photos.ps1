& {
    $repoPath = 'C:\Users\Peter\Documents\Development\NooklinPhotos'

    $git = (Get-Command git -ErrorAction SilentlyContinue).Source
    if (-not $git) { $git = "$env:ProgramFiles\Git\cmd\git.exe" }
    if (-not (Test-Path $git)) { throw 'git niet gevonden.' }

    function Check($what) { if ($LASTEXITCODE -ne 0) { throw "Mislukt: $what" } }

    Set-Location $repoPath

    if ((& $git rev-parse --abbrev-ref HEAD) -ne 'main') { throw 'Je zit niet op de branch main.' }
    if ((& $git remote get-url origin) -notmatch 'Nooklin/Photos') { throw 'origin wijst niet naar Nooklin/Photos.' }

    # Versie uit Gradle
    $gradle = Get-Content "$repoPath\app\build.gradle.kts" -Raw
    $name = [regex]::Match($gradle, 'versionName\s*=\s*"(\d+\.\d+\.\d+)"').Groups[1].Value
    $code = [int][regex]::Match($gradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value
    if (-not $name) { throw 'versionName (x.y.z) niet gevonden in app\build.gradle.kts. Er is niets gewijzigd.' }
    $tag = "v$name"

    if (-not (Test-Path "$repoPath\RELEASE_NOTES_$tag.md")) { throw "RELEASE_NOTES_$tag.md ontbreekt. Er is niets gewijzigd." }

    & $git fetch origin --tags
    Check 'git fetch'

    # Vergelijk met de hoogste eerdere tag
    $prev = & $git tag --list 'v*' --sort=-v:refname | Where-Object { $_ -match '^v\d+\.\d+\.\d+$' -and $_ -ne $tag } | Select-Object -First 1
    if ($prev) {
        if ([version]$name -le [version]$prev.TrimStart('v')) { throw "versionName $name is niet hoger dan $prev. Er is niets gewijzigd." }
        $oldGradle = (& $git show "${prev}:app/build.gradle.kts") -join "`n"
        Check "git show ${prev}:app/build.gradle.kts"
        $oldCode = [int][regex]::Match($oldGradle, 'versionCode\s*=\s*(\d+)').Groups[1].Value
        if ($code -le $oldCode) { throw "versionCode ($code) moet hoger zijn dan die van $prev ($oldCode). Er is niets gewijzigd." }
    }

    if (& $git ls-remote --tags origin $tag) { throw "Tag $tag staat al op GitHub. Er is niets gewijzigd." }

    # Niet achterlopen op GitHub
    & $git merge-base --is-ancestor origin/main HEAD
    if ($LASTEXITCODE -ne 0) { throw 'origin/main bevat commits die je lokaal niet hebt. Er is niets gepusht. Meld dit in de chat.' }

    # Commit (alleen als er iets te committen is)
    & $git add -A
    Check 'git add'
    & $git diff --cached --quiet
    if ($LASTEXITCODE -ne 0) {
        & $git commit -m "Nooklin Photos $name"
        Check 'git commit'
    }

    # Tag
    $head = & $git rev-parse HEAD
    Check 'git rev-parse HEAD'
    if (& $git tag -l $tag) {
        $tagCommit = & $git rev-parse "${tag}^{commit}"
        Check "tag $tag opzoeken"
        if ($tagCommit -ne $head) { throw "Tag $tag bestaat lokaal maar wijst naar $($tagCommit.Substring(0, 7)) in plaats van $($head.Substring(0, 7)). Er is niets gepusht. Meld dit in de chat." }
    } else {
        & $git tag $tag
        Check 'git tag'
    }

    & $git push origin main
    Check 'git push main'
    & $git push origin $tag
    Check 'git push tag'

    Write-Host ''
    Write-Host "Klaar: $tag is gepusht. Volg de build op https://github.com/Nooklin/Photos/actions"
}