# CPM Wheel (Fabric 1.20.1, CPM 0.6.27a)

Roda de toggles para o Customizable Player Models.
Segure a tecla (padrão: **V**), arraste o mouse na direção do toggle e solte.

- O botão de **baixo** é sempre o **Resetar** (desliga todos os toggles, volta os dropdowns para "Nenhum" e limpa gesto/pose).
- **Dropdowns** (Estilos, Expressões...) viram uma **sub-roda**; lá o botão de baixo é **Voltar**.
- **Value layers** (sliders) não aparecem.
- Mais de 7 itens: use a **rodinha do mouse** para trocar de página.
- Toque rápido na tecla (sem segurar): a roda fica aberta e você escolhe com **clique**. Esc ou clique direito fecha.
- Troque a tecla em Opções > Controles > "CPM Wheel".

## Ícones

Pasta: `.minecraft/config/cpm_wheel/icons/` (criada no primeiro uso, com um LEIA-ME.txt).
Coloque um PNG com o MESMO nome do toggle. Ex.: toggle "Oculos" -> `Oculos.png`.
Opções de dropdown: `NomeDoDropdown_NomeDaOpcao.png`. Reset: `_reset.png`. Voltar: `_back.png`.
Os ícones são relidos toda vez que a roda abre (não precisa reiniciar o jogo).

## Gerar o .jar (sem instalar nada)

1. Crie uma conta grátis em https://github.com
2. "New repository" > nome `cpm-wheel` > Public > Create.
3. "uploading an existing file" e arraste TODO o conteúdo desta pasta
   (inclusive as pastas `libs` e `.github`).
   Se o `.github` não subir, use "Add file > Create new file", digite o nome
   `.github/workflows/build.yml` e cole o conteúdo do arquivo.
4. "Commit changes". Abra a aba **Actions** e espere o "build" ficar verde (3 a 6 min).
5. Clique no build, role até **Artifacts** e baixe `cpm-wheel` (dentro do zip está o .jar).
6. Se ficar vermelho, clique no build, copie o erro e me mande.

## Instalar

Na pasta `mods`: este .jar + Fabric API + CPM 0.6.27a (Fabric 1.20.1).

## Se der problema no jogo

Mande o arquivo `.minecraft/logs/latest.log` (procure linhas com "CPM Wheel").
Este addon usa classes internas do CPM: se você atualizar o CPM, pode precisar de ajuste.
