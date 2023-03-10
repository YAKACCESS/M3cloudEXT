$commit=$args[0]

write-host "Looking for commit $($commit)" 

Remove-Item "ChangesReport - $($commit).diff"
$Changed = (git show $($commit) --name-only | Format-List) | Out-String

foreach ($file in $Changed.Split([Environment]::NewLine)){
	if($file -like 'src/main/*') {
		Write-host "Processing $($file)"  	
		$header = "=====Start $($file) ======================================================="
		$header | Out-File -FilePath "ChangesReport - $($commit).diff" -Append	
		#(git diff -U5 --color-moved-ws=ignore-all-space $file) | select -skip 4 | Out-File -FilePath "ChangesReport - $($commit).diff" -Append	
		(git show $($commit) -U5 -E -b -w -B -- $file) | Out-File -FilePath "ChangesReport - $($commit).diff" -Append	
		$header = "=====End $($file) =======================================================$([Environment]::NewLine)$([Environment]::NewLine)"
		$header | Out-File -FilePath "ChangesReport - $($commit).diff" -Append	
		}
	}
Invoke-Item "ChangesReport - $($commit).diff"