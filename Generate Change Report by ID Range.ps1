$StartCommit=$args[0]
$EndCommit=$args[1]

write-host "Looking for commit $($StartCommit) "
write-host "=> $($EndCommit)" 

Remove-Item "ChangesReport - $($StartCommit).$($EndCommit).diff"
$Changed = (git show $($StartCommit) $($EndCommit) --name-only | Format-List) | Out-String

foreach ($file in $Changed.Split([Environment]::NewLine)){
	if($file -like 'src/main/*') {
		Write-host "Processing $($file)"  	
		$header = "=====Start $($file) ======================================================="
		$header | Out-File -FilePath "ChangesReport - $($StartCommit).$($EndCommit).diff" -Append	
		#(git diff -U5 --color-moved-ws=ignore-all-space $file) | select -skip 4 | Out-File -FilePath "ChangesReport - $($StartCommit).diff" -Append	
		(git show $($StartCommit) $($EndCommit) -U5 -E -b -w -B -- $file) | Out-File -FilePath "ChangesReport - $($StartCommit).$($EndCommit).diff" -Append	
		$header = "=====End $($file) =======================================================$([Environment]::NewLine)$([Environment]::NewLine)"
		$header | Out-File -FilePath "ChangesReport - $($StartCommit).$($EndCommit).diff" -Append	
		}
	}
Invoke-Item "ChangesReport - $($StartCommit).$($EndCommit).diff"