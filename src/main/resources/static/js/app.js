document.addEventListener('DOMContentLoaded',()=>{
  document.querySelectorAll('[data-confirm]').forEach(btn=>btn.addEventListener('click',e=>{
    const msg=btn.getAttribute('data-confirm')||'Bạn có chắc chắn?';
    if(!confirm(msg)) e.preventDefault();
  }));
  document.querySelectorAll('[data-file-input]').forEach(input=>{
    const target=document.querySelector(input.getAttribute('data-file-input'));
    if(target) input.addEventListener('change',()=>{target.textContent=input.files.length?input.files[0].name:'Chưa chọn file';});
  });
});
