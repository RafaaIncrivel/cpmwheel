# CPM Wheel — Allowlist

Este repositório contém a lista de UUIDs autorizados a usar o CPM Wheel.

## Lista de acesso

O mod deve consultar o arquivo [allowed-uuids.json](allowed-uuids.json). Adicione UUIDs de Minecraft Java no formato de 32 caracteres, sem hífens:

```json
{
  "uuids": [
    "00000000000000000000000000000000"
  ]
}
```

O valor acima é somente um exemplo. A lista ainda será preenchida com os UUIDs autorizados.

URL bruta do arquivo: https://raw.githubusercontent.com/RafaaIncrivel/cpmwheel/main/allowed-uuids.json

## Privacidade e limites

Este repositório é público, então qualquer UUID inserido aqui ficará visível. Não adicione tokens ou senhas. Uma checagem somente no cliente pode ser removida por alguém que modifique o mod; proteção robusta exige validação no servidor.
