const { Octokit } = require("@octokit/rest");

const args = process.argv.slice(2);
const branch1 = args[0];
const branch2 = args[1];
const token = args[2];
const octokit = new Octokit({auth: token});

async function run() {
  const { data: pull1 } = await octokit.pulls.create({
    owner: 'anuken',
    repo: 'Mindustry',
    head: `mindustrykilla:${branch1}`,
    base: 'master',
    title: 'Hi',
  });
  const { data: pull2 } = await octokit.pulls.create({
    owner: 'anuken',
    repo: 'Mindustry',
    head: `mindustrykilla:${branch2}`,
    base: 'master',
    title: 'Hello',
  });
  octokit.issues.createComment({
    owner: 'anuken',
    repo: 'Mindustry',
    issue_number: pull1.number,
    body: 'mindustry is my favourite game because I just love to create fun things, its super inspiring for me and I love the idea of making something out of nothing.',
  });
  octokit.issues.createComment({
    owner: 'anuken',
    repo: 'Mindustry',
    issue_number: pull2.number,
    body: 'Do you remember when you were in fourth or fifth grade and every week you went to "Manila Pizza" with your family to watch American movies?',
  });
  while (true) {
    octokit.pulls.update({
       owner: 'anuken',
       repo: 'Mindustry',
       pull_number: pull1.number,
       state: 'open',
     });
     octokit.pulls.update({
       owner: 'anuken',
       repo: 'Mindustry',
       pull_number: pull2.number,
       state: 'open',
     });
     octokit.pulls.update({
       owner: 'anuken',
       repo: 'Mindustry',
       pull_number: pull1.number,
       state: 'closed',
     });
     octokit.pulls.update({
       owner: 'anuken',
       repo: 'Mindustry',
       pull_number: pull2.number,
       state: 'closed',
     });
  }
}
run();
