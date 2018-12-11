package cn.edu.ncu.bookstore.controller;

import cn.edu.ncu.bookstore.config.MyUserDetails;
import cn.edu.ncu.bookstore.entity.Book;
import cn.edu.ncu.bookstore.entity.Cart;
import cn.edu.ncu.bookstore.entity.CartKey;
import cn.edu.ncu.bookstore.entity.User;
import cn.edu.ncu.bookstore.repository.BookRepository;
import cn.edu.ncu.bookstore.repository.CartRepository;
import cn.edu.ncu.bookstore.repository.CategoryRepository;
import cn.edu.ncu.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sun.misc.Request;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class MainController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CartRepository cartRepository;

    public boolean isExistUser(){
        MyUserDetails myUserDetails = (MyUserDetails)SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return myUserDetails.getUsername() == null ? false : true;
    }

    //获取当前User
    public User getUser(){
        MyUserDetails myUserDetails = (MyUserDetails)SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userRepository.findById(myUserDetails.getUsername()).get();
    }

    //查词所有分类
    //@ModelAttribute 注释的方法每次调用该控制器类的请求处理方法时都会被调用 可添加全局变量
    @ModelAttribute
    public void selectCategory(Model model){
        model.addAttribute("categoryList", categoryRepository.findAll());
    }

    //记住用户登录状态
    @ModelAttribute
    public void addUser(Model model) {
        //判断用户是否已登录
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(!(auth instanceof AnonymousAuthenticationToken)){
            User user = getUser();
            String username = user.getUser_id();
            model.addAttribute("user", user);
            model.addAttribute("username", username);
        }
    }

    @GetMapping("/")
    public String root(){
        return "redirect:/index";
    }


    @GetMapping("/index")
    public String index(Model model){
        //model.addAttribute("categoryList", categoryRepository.findAll());
        return  "index";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/login-error")
    public String loginError(Model model){
        model.addAttribute("loginError", true);
        model.addAttribute("errorMsg", "登录失败，用户名或密码错误");
        return "login";
    }


    //添加到购物车
    @RequestMapping(value = "/addToCart", method = RequestMethod.POST)
    public String addToCart(Model model, Integer book_id, Integer book_amount) {
        User user = getUser();
        String user_id = user.getUser_id();
        Book book = bookRepository.findById(book_id).get();
        CartKey cart_id = new CartKey(user_id, book_id, 1);
        Cart cart;
        if(cartRepository.findById(cart_id).isPresent()) {
            //取出之前购物车的同类商品且cart_status为1
            cart = cartRepository.findById(cart_id).get();
            cart.setBook_amount(cart.getBook_amount() + book_amount);
        } else {
            cart = new Cart(user.getUser_id(), user, book.getBook_id(), book, 1, book_amount);
        }
        cartRepository.save(cart);
        return "redirect:json/true.json";
    }


    //查询所有图书
    @RequestMapping("/listAll")
    public String listAll(Model model){
        model.addAttribute("books", bookRepository.findAll());
        return "listBooks";
    }

    //查询分类图书
    @RequestMapping("/listBooks")
    public String listBooks(Model model, String categoryName){
        List<Book> books = bookRepository.findByBook_category(categoryName);
        model.addAttribute("books", books);
        return "listBooks";
    }

    //根据id查询图书
    @RequestMapping("/bookDetails")
    public String listBook(Model model, Integer book_id){
        Book book = bookRepository.findById(book_id).get();
        model.addAttribute("book", book);
        return "bookDetails";
    }

    //搜索框
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(Model model, Integer source, String keyword) {
        String[] sourceC = {"书名", "作者", "出版社"};
        List<Book> books;
        if(source == 0) {
            books = bookRepository.findByBook_nameLike(keyword);
        } else {
            books = source == 1 ? bookRepository.findByBook_author(keyword) : bookRepository.findByBook_publiser(keyword);
        }
        for(Book book : books) {
            System.out.println(book.getBook_name());
        }
        model.addAttribute("books", books);
        model.addAttribute("sourceC", sourceC[source]);
        model.addAttribute("keyword", keyword);
        model.addAttribute("books_num", books.size());
        return "search";
    }

    //查看购物车
    @RequestMapping(value = "/cart")
    public String cart(Model model) {
        User user = getUser();
        List<Cart> carts = cartRepository.findCartByUser(user);
        double price = 0;
        for(Cart cart : carts) {
            price += cart.getBook().getBook_price() * cart.getBook().getBook_discount() * cart.getBook_amount() * 0.1;
        }
        model.addAttribute("carts", carts);
        model.addAttribute("totalPrice", new DecimalFormat("0.00").format(price));

        return "cart";
    }

    //删除购物车中的某件商品
    @RequestMapping(value = "/deleteFromCart")
    public String deleteFromCart(Model model, Integer book_id) {
        User user = getUser();
        CartKey cartKey =  new CartKey(user.getUser_id(), book_id, 1);
        cartRepository.deleteById(cartKey);
        return "redirect:json/true.json";
    }

    //更新购物车
    @RequestMapping(value = "/updateCart")
    public String updateCart(Model model, Integer book_id, Integer book_amount) {
        User user = getUser();
        CartKey cartKey = new CartKey(user.getUser_id(), book_id, 1);
        Cart cart = cartRepository.findById(cartKey).get();
        cart.setBook_amount(book_amount);
        cartRepository.save(cart);
        List<Cart> carts = cartRepository.findCartByUser(user);
        double price = 0;
        for(Cart cart1 : carts) {
            price += cart1.getBook().getBook_price() * cart1.getBook().getBook_discount() * cart1.getBook_amount() * 0.1;
        }
        model.addAttribute("totalPrice", new DecimalFormat("0.00").format(price));
        return "redirect:json/true.json";
    }

}