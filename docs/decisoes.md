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