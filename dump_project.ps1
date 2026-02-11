# Nome del file di output
$outputFile = "project_dump.txt"

# Path della cartella del progetto (la cartella corrente)
$projectRoot = Get-Location

Write-Host "Creazione del dump del progetto in: $outputFile"

# Pulisce il file di output se esiste già
if (Test-Path $outputFile) {
    Remove-Item $outputFile
}

# --- 1. STRUTTURA DELLE CARTELLE ---
"============================================================" | Out-File -FilePath $outputFile -Encoding utf8
"               STRUTTURA DEL PROGETTO (tree /F)               " | Out-File -FilePath $outputFile -Encoding utf8 -Append
"============================================================" | Out-File -FilePath $outputFile -Encoding utf8 -Append
# Esegue il comando 'tree' e salva l'output nel file
cmd /c "tree /F" | Out-File -FilePath $outputFile -Encoding utf8 -Append
"`n`n" | Out-File -FilePath $outputFile -Encoding utf8 -Append


# --- 2. CONTENUTO DEI FILE SORGENTE ---
# Lista delle estensioni di file che vogliamo includere
$includeExtensions = @("*.java", "*.xml", "*.properties", "*.gradle")

# Trova tutti i file corrispondenti, escludendo le cartelle 'target' e '.settings'
Get-ChildItem -Path $projectRoot -Recurse -Include $includeExtensions | Where-Object {
    $_.FullName -notmatch "[\\/]target[\\/]" -and $_.FullName -notmatch "[\\/]\.settings[\\/]"
} | ForEach-Object {
    # Ottiene il path relativo del file per una migliore leggibilità
    $relativePath = $_.FullName.Substring($projectRoot.Path.Length + 1)

    "------------------------------------------------------------" | Out-File -FilePath $outputFile -Encoding utf8 -Append
    "--- FILE: $relativePath ---" | Out-File -FilePath $outputFile -Encoding utf8 -Append
    "------------------------------------------------------------" | Out-File -FilePath $outputFile -Encoding utf8 -Append
    "`n" | Out-File -FilePath $outputFile -Encoding utf8 -Append

    # Aggiunge il contenuto del file
    Get-Content -Path $_.FullName | Out-File -FilePath $outputFile -Encoding utf8 -Append

    "`n`n" | Out-File -FilePath $outputFile -Encoding utf8 -Append
}

Write-Host "Fatto! Il file '$outputFile' e' stato creato con successo."
Write-Host "Ora puoi aprire il file e copiare tutto il suo contenuto."