<#

#>

del ./PendingCommitReport.diff
$Changed = (git diff --name-only | Format-List) | Out-String

foreach ($file in $Changed.Split([Environment]::NewLine)){
	if($file -like 'src/main/*') {
		Write-host "Processing $($file)"  	
		$header = "=====Start $($file) ======================================================="
		$header | Out-File -FilePath PendingCommitReport.diff -Append	
		#(git diff -U5 --color-moved-ws=ignore-all-space $file) | select -skip 4 | Out-File -FilePath PendingCommitReport.diff -Append	
		(git diff -U5 -E -b -w -B $file) | select -skip 4 | Out-File -FilePath PendingCommitReport.diff -Append	
		$header = "=====End $($file) =======================================================$([Environment]::NewLine)$([Environment]::NewLine)"
		$header | Out-File -FilePath PendingCommitReport.diff -Append	
		}
	}

	foreach ($file in $Changed.Split([Environment]::NewLine)){
		if($file -like '*.md') {
			Write-host "Processing $($file)"  	
			$header = "=====Start $($file) ======================================================="
			$header | Out-File -FilePath PendingCommitReport.diff -Append	
			#(git diff -U5 --color-moved-ws=ignore-all-space $file) | select -skip 4 | Out-File -FilePath PendingCommitReport.diff -Append	
			(git diff -U5 -E -b -w -B $file) | select -skip 4 | Out-File -FilePath PendingCommitReport.diff -Append	
			$header = "=====End $($file) =======================================================$([Environment]::NewLine)$([Environment]::NewLine)"
			$header | Out-File -FilePath PendingCommitReport.diff -Append	
			}
		}

Invoke-Item "PendingCommitReport.diff"