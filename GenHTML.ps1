



Get-ChildItem "." -Filter *.md | 
Foreach-Object {
    #$content = Get-Content $_.FullName
    

    #filter and save content to the original file
    #$content | Where-Object {$_ -match 'step[49]'} | Set-Content $_.FullName

    #filter and save content to a new file 
    #$content | Where-Object {$_ -match 'step[49]'} | Set-Content ($_.BaseName + '_out.log')

    $markdown = ConvertFrom-Markdown -Path $_.FullName
    $outfile = $_.FullName -replace ".md", ".html"
    $markdown.Html | Out-File -Encoding utf8 $outfile
}