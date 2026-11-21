# Registro de Decisões

## D01 – Java 25 + Maven
**Decisão:** usar JDK 25 (LTS) e Maven para build.
**Motivo:** versão LTS já instalada; Maven gerencia o driver do SQLite.

## D02 – SQLite
**Decisão:** banco SQLite via JDBC (sqlite-jdbc).
**Motivo:** banco em arquivo, sem servidor; ideal para aprendizado.

## D03 – Pessoa e Leitor abstratas
**Decisão:** Pessoa e Leitor são classes abstratas
**Motivo:** Não existem pessoa genérica, nem leitor genérico. Uma pessoa necessariamente será um Leitor ou um Funcionário. E um Leitor necessariamente será um Aluno ou um Professor.

## D04 – BigDecimal para multa
**Decisão:** Usamos BigDecimal para multa em vez de double
**Motivo:** double pode ocasionar erros de arredondamento, que não podem ocorrer quando se trata de valor monetário. 

## D05 - ENUM para situações
**Decisão:** Situações/Status viraram ENUM. 
**Motivo:** Se torna impossível escrever o status errado e salvar no sistema, o compilador barra. 

## D06 – Login de funcionários
**Decisão:** o sistema exige login e senha de um funcionário para ser acessado, e o funcionário logado é registrado automaticamente nos empréstimos (RN08 e RN09).
**Motivo:** controla quem usa o sistema e garante rastreabilidade: cada empréstimo fica associado a quem o realizou, sem depender de o usuário escolher o nome numa lista.

## D07 – Senha armazenada com hash PBKDF2 e salt
**Decisão:** a senha nunca é salva em texto puro. O banco guarda apenas o salt (aleatório, um por funcionário) e o hash gerado com PBKDF2WithHmacSHA256. No login, a senha digitada passa pelo mesmo cálculo e os hashes são comparados.
**Motivo:** se o arquivo do banco vazar, as senhas não podem ser lidas. O salt impede que senhas iguais gerem hashes iguais, e o PBKDF2, por ser lento de propósito, dificulta ataques de força bruta. Foi escolhido em vez de SHA-256 simples (rápido demais para senhas) e de bibliotecas externas como BCrypt, porque já faz parte do Java e não exige dependência nova.

## D08 – Interface gráfica gerada com auxílio de IA
**Decisão:** o layout das telas Swing foi gerado com auxílio de IA. O tratamento de eventos e de exceções na camada de UI foi implementado e revisado pelo autor.
**Motivo:** o foco do projeto é o back-end e os conceitos de POO; a geração do layout liberou tempo para essas partes.

## D09 – Id como Integer
**Decisão:** o atributo `id` das entidades é `Integer`, e não `int`.
**Motivo:** um objeto recém-criado ainda não foi salvo no banco e, portanto, não tem id. Com `Integer`, esse estado é representado por `null`; com `int`, seria `0`, um valor que parece válido.

## D10 – Dois construtores por entidade
**Decisão:** cada entidade tem um construtor sem `id` (objeto novo) e outro com `id` (objeto lido do banco). O primeiro chama o segundo, passando `null` no lugar do id.
**Motivo:** evita passar `null` manualmente ao cadastrar e mantém as atribuições escritas em um único lugar (encadeamento de construtores com `this(...)` e `super(...)`).

## D11 – Método getTipo() em Leitor
**Decisão:** `Leitor` declara o método abstrato `getTipo()`, implementado por Aluno ("ALUNO") e Professor ("PROFESSOR").
**Motivo:** permite gravar os dois tipos na mesma tabela do banco, com uma coluna `tipo`, sem usar `instanceof`. Novos tipos de leitor não exigem mudanças no código que já existe.

## D12 – Relação bidirecional entre Obra e Copia
**Decisão:** a Obra mantém a lista de suas cópias e cada Copia guarda uma referência à sua Obra. A lista é exposta apenas como somente leitura e alterada só por `adicionarCopia()`.
**Motivo:** as telas precisam exibir o título da obra a partir de uma cópia, e o DAO precisa saber a qual obra cada cópia pertence. Centralizar a inclusão em `adicionarCopia()` garante que os dois lados da relação fiquem sempre consistentes.

## D13 – Fábrica de conexões (ConexaoFactory)
**Decisão:** todas as conexões com o banco são obtidas por um único método estático, `ConexaoFactory.getConexao()`, que também liga a verificação de chaves estrangeiras (`PRAGMA foreign_keys = ON`).
**Motivo:** concentra em um só lugar o endereço do banco e a configuração da conexão. Se o banco mudar (por exemplo, para PostgreSQL), apenas essa classe é alterada. O SQLite não verifica chaves estrangeiras por padrão, e isso precisa ser ligado a cada conexão.

## D14 – Mapeamento da herança para o banco
**Decisão:** `Pessoa` não tem tabela própria; seus campos aparecem nas tabelas `leitor` e `funcionario` (tabela por classe concreta). `Aluno` e `Professor` dividem a tabela `leitor`, diferenciados pela coluna `tipo` (tabela única).
**Motivo:** bancos relacionais não têm herança. `Pessoa` é abstrata e nunca existe sozinha. Aluno e Professor têm exatamente os mesmos dados (o que muda entre eles é o comportamento, implementado no Java), então uma tabela única evita duplicação e consultas com junções. O valor da coluna `tipo` vem de `getTipo()`, sem uso de `instanceof`.

## D15 – Tipos de dados no SQLite
**Decisão:** datas são gravadas como `TEXT` no formato ISO (`2026-11-21`); valores monetários como `TEXT` (`"3.00"`); enums como `TEXT` com o nome da constante.
**Motivo:** o SQLite tem poucos tipos nativos. O formato ISO é o usado por `LocalDate.toString()` e `LocalDate.parse()` e ordena corretamente como texto. Gravar a multa como `REAL` reintroduziria a imprecisão do `double`, que foi evitada com `BigDecimal`. Enums são convertidos com `name()` e `valueOf()`.

## D16 – Restrições no próprio banco
**Decisão:** as tabelas usam `NOT NULL`, `UNIQUE` (CPF, matrícula, login, ISBN, código da cópia), `CHECK` (tipo de leitor) e chaves estrangeiras (`REFERENCES`).
**Motivo:** além das validações feitas no Java, o banco garante a integridade dos dados como uma segunda linha de defesa: não aceita duplicatas, valores inválidos nem registros que apontem para outros inexistentes.

## D17 – Criação automática das tabelas
**Decisão:** a classe `InicializadorBanco` cria as tabelas com `CREATE TABLE IF NOT EXISTS` sempre que o programa inicia.
**Motivo:** o sistema funciona em qualquer máquina sem configuração manual: na primeira execução o arquivo `biblioteca.db` e as tabelas são criados; nas seguintes, nada é alterado. O arquivo do banco não é versionado no Git, pois é gerado pelo próprio programa.

## D18 – NegocioException checked, separada de SQLException
**Decisão:** violações de regras de negócio são sinalizadas por `NegocioException`, que herda de `Exception` (checked). Falhas técnicas do banco continuam como `SQLException`.
**Motivo:** por ser checked, o compilador obriga as telas a tratar a exceção, atendendo à exigência de controle de erros perto da UI. Separar regra violada ("leitor com atraso") de falha técnica ("banco indisponível") permite que a tela trate cada caso de forma diferente. Foi descartada uma exceção unchecked, que permitiria a uma tela esquecer o `try/catch` e o programa quebrar diante do usuário.

## D19 – Sem transações entre gravações (limitação consciente)
**Decisão:** operações com várias gravações (por exemplo, empréstimo + situação da cópia) usam uma conexão por gravação, sem transação.
**Motivo:** simplicidade no escopo do projeto. O risco é conhecido: se o programa fosse interrompido entre duas gravações, os dados poderiam ficar inconsistentes. A solução profissional seria executar as gravações numa única transação (`setAutoCommit(false)`, `commit` e `rollback`), compartilhando a mesma conexão entre os DAOs.

## D20 – Administrador criado automaticamente
**Decisão:** ao iniciar, `FuncionarioService.garantirAdministrador()` cria o funcionário `admin` (senha `admin123`) se não houver nenhum funcionário cadastrado.
**Motivo:** o banco é criado vazio em qualquer máquina nova; sem isso, ninguém conseguiria fazer o primeiro login. A senha inicial é gravada com hash, como todas as outras.

## D21 – O service decide entre inserir e atualizar
**Decisão:** os services de cadastro têm um único método `salvar`, que chama `INSERT` quando o id do objeto é `null` e `UPDATE` quando o id existe.
**Motivo:** a tela não precisa saber se o registro é novo ou existente. Isso aproveita a decisão D09 (`id` como `Integer`, com `null` significando "ainda não salvo").

## D22 – FlatLaf, com tema claro/escuro e identidade em verde oliva
**Decisão:** a interface usa a biblioteca FlatLaf (`com.formdev:flatlaf` 3.7) no lugar do visual padrão do Swing, com cor de destaque verde oliva e botão para alternar entre tema claro e escuro em tempo real.
**Motivo:** o visual padrão do Swing (Metal) é datado e igual ao da maioria dos trabalhos. A FlatLaf moderniza todos os componentes com poucas linhas de código, sem exigir desenho manual de cada um. Foi descartado o Nimbus (já incluso no Java) por oferecer menos personalização e não ter tema escuro.

## D23 – Telas herdam da classe abstrata PainelTela
**Decisão:** toda tela da barra lateral estende `PainelTela`, que declara o método abstrato `atualizar()` e oferece o método herdado `criarCabecalho(...)`. A `MainFrame` guarda as telas num `Map<String, PainelTela>` e chama `atualizar()` ao exibir qualquer uma.
**Motivo:** a janela principal não precisa conhecer cada tela (polimorfismo), e adicionar uma tela nova não exige alterar a lógica de navegação. O cabeçalho padrão fica escrito uma única vez.

## D24 – ModeloTabela<T> genérico (Template Method)
**Decisão:** os modelos de tabela estendem a classe abstrata genérica `ModeloTabela<T>`, que gerencia a lista, as linhas e os nomes das colunas. Cada subclasse implementa apenas `valorDaColuna(item, coluna)`.
**Motivo:** evita repetir um `AbstractTableModel` completo para cada entidade (leitores, obras, cópias, empréstimos, reservas). O `LeitorTableModel` foi refatorado para usar a nova base sem mudar o comportamento da tela.

## D25 – Componentes desenhados por código
**Decisão:** o painel de marca do login, os cartões de indicadores, o fundo dos formulários e os selos de situação nas tabelas são desenhados com `paintComponent` e `Graphics2D`, em classes que estendem componentes Swing (`JPanel`, `DefaultTableCellRenderer`).
**Motivo:** dá identidade visual ao sistema e destaca informações (situação de cópias, empréstimos e reservas) por cor. Também serve de exemplo de herança e sobrescrita aplicadas à interface.

## D26 – Formatação de datas e moeda somente na view
**Decisão:** o banco e o model trabalham com formatos técnicos (datas ISO `2026-11-28`, valores `3.00`); a conversão para o formato brasileiro (`28/11/26`, `R$ 3,00`) é feita apenas nas telas, com `DateTimeFormatter` e `NumberFormat`.
**Motivo:** formatação é uma questão de apresentação. Mantê-la na view evita que mudanças visuais afetem regras de negócio ou o armazenamento, e respeita a separação de responsabilidades entre as camadas.