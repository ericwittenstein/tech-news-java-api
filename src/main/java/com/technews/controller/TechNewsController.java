package com.technews.controller;

import com.technews.model.Comment;
import com.technews.model.Post;
import com.technews.model.User;
import com.technews.model.Vote;
import com.technews.repository.CommentRepository;
import com.technews.repository.PostRepository;
import com.technews.repository.UserRepository;
import com.technews.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class TechNewsController {
    @Autowired
    PostRepository postRepository;

    @Autowired
    VoteRepository voteRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CommentRepository commentRepository;

    final String noticeText = "notice";
    final String loginText = "login";

    @PostMapping("/users/login")
    public String login(@ModelAttribute User user, Model model, HttpServletRequest request) throws Exception {


        if ((user.getPassword().equals(null) || user.getPassword().isEmpty()) || (user.getEmail().equals(null) || user.getPassword().isEmpty())) {
            model.addAttribute(noticeText, "Email address and password must be populated in order to login!");
            return loginText;
        }

        User sessionUser = userRepository.findUserByEmail(user.getEmail());

        try {
            // if sessionUser is invalid, running .equals() will throw an error
            if (sessionUser.equals(null)) {

            }
            // We will catch an error and notify the client that email address is not recognized
        } catch (NullPointerException e) {
            model.addAttribute(noticeText, "Email address is not recognized!");
            return loginText;
        }

        // Validate password
        String sessionUserPassword = sessionUser.getPassword();
        boolean isPasswordValid = BCrypt.checkpw(user.getPassword(), sessionUserPassword);
        if (!isPasswordValid) {
            model.addAttribute(noticeText, "Password is not valid!");
            return loginText;
        }

        sessionUser.setLoggedIn(true);
        request.getSession().setAttribute("SESSION_USER", sessionUser);

        return "redirect:/dashboard";
    }

    @PostMapping("/users")
    public String signup(@ModelAttribute User user, Model model, HttpServletRequest request) throws Exception {

        if ((user.getUsername().equals(null) || user.getUsername().isEmpty()) || (user.getPassword().equals(null) || user.getPassword().isEmpty()) || (user.getEmail().equals(null) || user.getPassword().isEmpty())) {
            model.addAttribute(noticeText, "In order to signup username, email address and password must be populated!");
            return loginText;
        }

        try {
            // Encrypt password
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute(noticeText, "Email address is not available! Please choose a different unique email address.");
            return loginText;
        }

        User sessionUser = userRepository.findUserByEmail(user.getEmail());

        try {
            if (sessionUser.equals(null)) {

            }
        } catch (NullPointerException e) {
            model.addAttribute(noticeText, "User is not recognized!");
            return loginText;
        }

        sessionUser.setLoggedIn(true);
        request.getSession().setAttribute("SESSION_USER", sessionUser);

        return "redirect:/dashboard";
    }

    @PostMapping("/posts")
    public String addPostDashboardPage(@ModelAttribute Post post, Model model, HttpServletRequest request) {

        if ((post.getTitle().equals(null) || post.getTitle().isEmpty()) || (post.getPostUrl().equals(null) || post.getPostUrl().isEmpty())) {
            return "redirect:/dashboardEmptyTitleAndLink";
        }

        if (request.getSession(false) == null) {
            return "redirect:/login";
        } else {
            User sessionUser = (User) request.getSession().getAttribute("SESSION_USER");
            post.setUserId(sessionUser.getId());
            postRepository.save(post);

            return "redirect:/dashboard";
        }
    }

    @PostMapping("/posts/{id}")
    public String updatePostDashboardPage(@PathVariable int id, @ModelAttribute Post post, Model model, HttpServletRequest request) {

        if (request.getSession(false) == null) {
            model.addAttribute("user", new User());
            return "redirect/dashboard";
        } else {
            Post tempPost = postRepository.getReferenceById(id);
            tempPost.setTitle(post.getTitle());
            postRepository.save(tempPost);

            return "redirect:/dashboard";
        }
    }

    @PostMapping("/comments")
    public String createCommentCommentsPage(@ModelAttribute Comment comment, Model model, HttpServletRequest request) {

        if (comment.getCommentText().isEmpty() || comment.getCommentText().equals(null)) {
            return "redirect:/singlePostEmptyComment/" + comment.getPostId();
        } else {
            if (request.getSession(false) != null) {
                User sessionUser = (User) request.getSession().getAttribute("SESSION_USER");
                comment.setUserId(sessionUser.getId());
                commentRepository.save(comment);
                return "redirect:/post/" + comment.getPostId();
            } else {
                return loginText;
            }
        }
    }

    @PostMapping("/comments/edit")
    public String createCommentEditPage(@ModelAttribute Comment comment, HttpServletRequest request) {

        if (comment.getCommentText().equals("") || comment.getCommentText().equals(null)) {
            return "redirect:/editPostEmptyComment/" + comment.getPostId();
        } else {
            if (request.getSession(false) != null) {
                User sessionUser = (User) request.getSession().getAttribute("SESSION_USER");
                comment.setUserId(sessionUser.getId());
                commentRepository.save(comment);

                return "redirect:/dashboard/edit/" + comment.getPostId();
            } else {
                return "redirect:/login";
            }
        }

    }

    @PutMapping("/posts/upvote")
    public void addVoteCommentsPage(@RequestBody Vote vote, HttpServletRequest request, HttpServletResponse response) {

        if (request.getSession(false) != null) {
            Post returnPost = null;
            User sessionUser = (User) request.getSession().getAttribute("SESSION_USER");
            vote.setUserId(sessionUser.getId());
            voteRepository.save(vote);

            returnPost = postRepository.getReferenceById(vote.getPostId());
            returnPost.setVoteCount(voteRepository.countVotesByPostId(vote.getPostId()));
        }
    }
}
