# Maze Slug

Jogo de labirinto em Java Swing construído a partir dos exemplos da Aula 1. O mapa vem de um arquivo texto; paredes, piso, saída, personagem e tanque são `JLabel` criados em tempo de execução.

## Mecânicas

- Movimento contínuo com WASD ou setas
- Personagem e tanque em GIF animado
- Tanque com movimento e reflexão nos obstáculos pela lógica de Pong
- Colisão por `Rectangle.intersects`
- Encostar numa parede ou no tanque encerra a tentativa
- Chegar à saída conclui a missão
- `R` reinicia e `ESC` fecha o jogo
- Thread dedicada atualiza o jogo em aproximadamente 60 quadros por segundo
- Efeitos sonoros com `AudioSystem`, `AudioInputStream` e `Clip`

## Executar no NetBeans

Abra a pasta como projeto Maven e execute `br.com.estevao.mazeslug.MazeGame`.

Requisitos: JDK 11 ou superior.

## Executar pelo terminal

Com Maven:

```bash
mvn package
java -jar target/maze-slug-1.0.0.jar
```

Com Ant:

```bash
ant run
```

Para gerar o JAR com Ant:

```bash
ant jar
java -jar dist/maze-slug.jar
```

## Mapa

O arquivo `src/main/resources/rsc/mapa.txt` aceita:

- `#`: parede
- `P`: início do jogador
- `T`: início do tanque
- `E`: saída
- espaço: piso

Todas as linhas precisam ter o mesmo tamanho e deve existir exatamente um `P`, um `T` e um `E`.

## Recursos visuais

Os sprites e tiles incluídos foram produzidos especificamente para este projeto. A direção visual é de arcade run-and-gun 16-bit; nenhum sprite proprietário de Metal Slug foi redistribuído.

Referências de recursos CC0 pesquisadas para a atividade:

- Kenney Brick Pack: https://kenney.nl/assets/brick-pack
- Kenney Topdown Tanks: https://opengameart.org/content/topdown-tanks
- Sullivan Tank Sprite: https://opengameart.org/content/tank-sprite
